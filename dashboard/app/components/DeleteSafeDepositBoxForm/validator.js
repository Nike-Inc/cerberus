const validate = values => {
    const errors = {}

    // validate that the name matches sdb name
    if (! values.verifiedSdbName || values.verifiedSdbName !== values.sdbName) {
        errors.verifiedSdbName = 'You must confirm that you want to delete the sdb by typing its name'
    }

    return errors
}

export default validate