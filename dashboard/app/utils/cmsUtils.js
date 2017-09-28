export function parseCMSError(response) {
    let msg = 'Server did not respond with message, checkout the console for full response'

    try {
        if (response.data.errors[0].message) {
            msg = response.data.errors[0].message
        }
    } catch(TypeError) {
        // continue
    }
    
    return msg
}