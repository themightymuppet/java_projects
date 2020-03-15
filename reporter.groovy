import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.bc.user.search.UserSearchService
import com.onresolve.scriptrunner.db.DatabaseUtil
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.event.type.EventDispatchOption
import groovy.sql.Sql
import java.sql.Driver

botUser = ''
customfield_x = ''
dbConnect = ''

if (issue.reporterId != botUser) {

    def driver = Class.forName('org.postgresql.Driver').newInstance() as Driver 

    MutableIssue issue = issue as MutableIssue
    String reporter_id = issue.getReporter().getEmailAddress()
    
    DatabaseUtil.withSql(dbConnect) {
        sql ->
        
        String details_query = "SELECT location, department, department_group, team, supervisor_id FROM " + table + 
            "WHERE status = 'Active' AND work_email = '" + reporter_id + "' order by dw_id desc;"

        groovy.sql.GroovyRowResult result = sql.firstRow(details_query)
        def locationID = result.getProperty("location")
        def departmentID = result.getProperty("department")
        def groupID = result.getProperty("department_group")
        def teamID = result.getProperty("team")
        def supervisor_id = result.getProperty("supervisor_id")
        
        String manager_query = "SELECT work_email FROM " + table " WHERE status = 'Active' AND employee_id = '" +
            supervisor_id + "' order by dw_id desc;"
        
        groovy.sql.GroovyRowResult result2 = sql.firstRow(manager_query)
        String managerEmailValue = result2.getProperty("work_email")

        sql.close()

        def customFieldManager = ComponentAccessor.getCustomFieldManager()
        def userSearchService = ComponentAccessor.getComponent(UserSearchService)
        def users = userSearchService.findUsersByEmail(managerEmailValue)

        if(locationID) {
            issue.setCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(customfield_x), locationID)
            issue.setCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(customfield_x), departmentID)
            issue.setCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(customfield_x), groupID)
            issue.setCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(customfield_x), teamID)

            CustomField managerField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(customfield_x)

            users.each {
                aUser -> issue.setCustomFieldValue(managerField, aUser);
            }

        }
	}
}