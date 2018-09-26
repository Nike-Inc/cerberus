export function parseCMSError(response) {
    let msg = 'Connection timed out. Check console for full error response.'

    try {
        if (response.data.errors[0].message) {
            msg = response.data.errors[0].message
        }
    } catch(TypeError) {
        // continue
    }
    
    return msg
}