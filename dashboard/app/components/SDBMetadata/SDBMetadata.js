import React from 'react'
import {Component} from 'react'
import './SDBMetadata.scss'


export default class SDBMetadata extends Component {

    render() {
        const {sdbMetadata} = this.props

        return (
            <div className="sdb-metadata-container">
                <div className="sdb-metadata">
                    <div className="sdb-metadata-kv">
                        <div className="sdb-metadata-label ncss-brand">Name:</div>
                        <div className="sdb-metadata-value">{sdbMetadata.name}</div>
                    </div>
                    <div className="sdb-metadata-kv">
                        <div className="sdb-metadata-label ncss-brand">Secret Path:</div>
                        <div className="sdb-metadata-value">{sdbMetadata.path}</div>
                    </div>
                    <div className="sdb-metadata-kv">
                        <div className="sdb-metadata-label ncss-brand">Owner:</div>
                        <div className="sdb-metadata-value">{sdbMetadata.owner}</div>
                    </div>
                </div>
                <div className="sdb-metadata">
                    <div className="sdb-metadata-kv">
                        <div className="sdb-metadata-label ncss-brand">Created By:</div>
                        <div className="sdb-metadata-value">{sdbMetadata.created_by}</div>
                    </div>
                    <div className="sdb-metadata-kv">
                        <div className="sdb-metadata-label ncss-brand">Created:</div>
                        <div className="sdb-metadata-value">{sdbMetadata.created_ts}</div>
                    </div>
                </div>
                <div className="sdb-metadata">
                    <div className="sdb-metadata-kv">
                        <div className="sdb-metadata-label ncss-brand">Last Updated By:</div>
                        <div className="sdb-metadata-value">{sdbMetadata.last_updated_by}</div>
                    </div>
                    <div className="sdb-metadata-kv">
                        <div className="sdb-metadata-label ncss-brand">Last Updated:</div>
                        <div className="sdb-metadata-value">{sdbMetadata.last_updated_ts}</div>
                    </div>
                </div>
                <div className="sdb-metadata">
                    <div className="sdb-metadata-kv">
                        <div className="sdb-metadata-label ncss-brand">Description:</div>
                        <div className="sdb-metadata-value desc">{sdbMetadata.description}</div>
                    </div>
                </div>
                <div className="sdb-metadata">
                    <div className="sdb-metadata-kv">
                        <div className="sdb-metadata-label ncss-brand">User Group Permissions:</div>
                        <div className="sdb-metadata-value">
                            { getPermissionsAsString(sdbMetadata.user_group_permissions) }
                        </div>
                    </div>
                </div>
                <div className="sdb-metadata">
                    <div className="sdb-metadata-kv">
                        <div className="sdb-metadata-label ncss-brand">IAM Principal Permissions:</div>
                        <div className="sdb-metadata-value">
                            { getPermissionsAsString(sdbMetadata.iam_role_permissions) }
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}

const getPermissionsAsString = (permissions) => {
    let string = Object.keys(permissions).map((key) => {
        return `${key}: ${permissions[key]}`
    }).join("\n")

    return string ? string : "No permissions defined"
}