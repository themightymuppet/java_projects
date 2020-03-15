import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.bc.user.search.UserSearchService
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.event.type.EventDispatchOption
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.ContentType.*
import groovyx.net.http.ContentType
import static groovyx.net.http.Method.*
import groovy.json.JsonSlurper
import net.sf.json.groovy.JsonSlurper

import org.apache.log4j.Category
 
def Category log = Category.getInstance("com.onresolve.jira.groovy")
log.debug "debug statements"

botUser = ''
oktaURL = ''
oktaAPI = ''
director_customfield = ''

if (issue.reporterId != botUser) {

    MutableIssue issue = issue as MutableIssue
    String userEmail = issue.getReporter().getEmailAddress()
    peopleManager = false

    while (peopleManager == false) {
        def http = new HTTPBuilder( oktaURL + '/api/v1/users/' + userEmail)
        http.request(GET) {
            requestContentType = ContentType.JSON
            headers = ['Authorization': oktaAPI]

                response.success = { resp, JSON ->

                   def profile = JSON.profile
                    if (profile.peopleManagerType == 'Director' | profile.peopleManagerType == 'Executive') {
                        peopleManager = true
                    } else {
                        userEmail = profile.managerEmail
                        log.debug userEmail
                    }
                }

        }
        }
    	def directorEmail = userEmail
   		log.debug 'Director Email is ' + directorEmail
  
    	def customFieldManager = ComponentAccessor.getCustomFieldManager()
        def userSearchService = ComponentAccessor.getComponent(UserSearchService)
        def users = userSearchService.findUsersByEmail(directorEmail)
        
        ApplicationUser svcUser = ComponentAccessor.getUserManager().getUserByName(botUser)
        IssueManager issueManager = ComponentAccessor.getIssueManager()

        CustomField directorField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(director_customfield)

        users.each {
           aUser -> issue.setCustomFieldValue(directorField, aUser);
        }
    
    	issueManager.updateIssue(svcUser, issue, EventDispatchOption.ISSUE_UPDATED, false)
	}
