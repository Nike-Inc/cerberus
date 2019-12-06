import * as loglevel from 'loglevel'

var level = localStorage.getItem('loglevel:cerberus') || 'INFO'
var log = loglevel.getLogger('cerberus')
log.setLevel(level)

export default log

export function getLogger(name) {
    var level = localStorage.getItem(`loglevel:${name}`) || 'INFO'
    var log = loglevel.getLogger(name)
    log.setLevel(level)
    return log
}