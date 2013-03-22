package com.developerb.healthcontrol

import static com.developerb.healthcontrol.HealthLevel.DEAD
import grails.converters.JSON
import grails.converters.XML

import org.codehaus.groovy.grails.commons.GrailsApplication


class HealthControlController {

    static scope = "singleton"

    GrailsApplication grailsApplication
    HealthControlService healthControlService


    def beforeInterceptor = {
        if(isSecretRequired()) {
            def providedSecret = request.getParameter("secret")
            if (providedSecret != grailsApplication.config.healthControl.secret) {
                response.status = 401
                return false
            }
        }
        if (!healthControlService.hasAtLeastOneHealthControl()) {
            response.status = 501
            return false
        }
    }


    def index() {
        def reports = healthControlService.reportAll()
        def appName = grailsApplication.metadata.get("app.name")

        if (reports.any { it.level == DEAD }) {
            response.status = grailsApplication.config.healthControl.deadStatus?: 500
        }

        def data = [
            healthReports: reports,
            applicationName : appName
        ]

        withFormat {
            html { data }
            xml { render simplify(data) as XML }
            json { render simplify(data) as JSON }
        }
    }

    private simplify(Map<String, Object> input) {
        return [
            applicationName: input.applicationName,
            healthReports: simplifyReports(input.healthReports)
        ]
    }

    private simplifyReports(List<HealthReport> reports) {
        return reports.collect { report ->
            [
                name: report.name,
                description: report.description,

                level: report.stateOfHealth.level.toString(),
                message: report.stateOfHealth.message,
                trouble: report.stateOfHealth.trouble,
                props: report.stateOfHealth.properties
            ]
        }
    }

    private boolean isSecretRequired() {
        def req = grailsApplication.config.healthControl.requireSecret
        return req instanceof Boolean? req: true
    }
}
