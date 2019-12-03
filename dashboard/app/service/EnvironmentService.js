/**
 * Service for determining the running environment and domain to use for Cerberus API Calls.
 */
export default class EnvironmentService {

    static getEnvironment() {
        var url = document.URL

        var re = /(\w+:\/\/)(.*?)\/.*/g
        var m
        var host

        while ((m = re.exec(url)) !== null) {
            if (m.index === re.lastIndex) {
                re.lastIndex++
            }
            host = m[2]
        }

        var env
        if(host.startsWith('localhost') || host.startsWith('127.0.0.1')) {
            env = 'local'
        } else {
            var pieces = host.split('.')
            env = pieces[0]
        }

        return env
    }

    /**
     *  Parses the URL to get the domain of the SMaaS
     */
    static getDomain() {
        var url = document.URL

        var re = /(\w+:\/\/.*?)\/.*/g
        var m
        var domain

        while ((m = re.exec(url)) !== null) {
            if (m.index === re.lastIndex) {
                re.lastIndex++
            }
            domain = m[1]
        }

        return domain
    }
}