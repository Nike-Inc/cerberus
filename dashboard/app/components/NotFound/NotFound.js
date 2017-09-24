import React from 'react'
import { Component } from 'react'
import { hashHistory } from 'react-router'
import { connect } from 'react-redux'

@connect()
export default class NotFound extends Component {
    componentDidMount() {
        hashHistory.push('/')
    }

    render() {
        return <h1>Not found</h1>
    }
}