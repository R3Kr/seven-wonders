import React, { Component } from 'react'
import { connect } from 'react-redux'
import { Heading, InlineForm } from 'rebass'
import { Link } from 'react-router'
class HomePage extends Component {

  play = (e) => {
    e.preventDefault()
    if (this._username !== undefined) {
      this.props.enterGame(this._username)
    }
  }

  render() {
    return (
      <div>
        <Heading>Enter your username to start playing!</Heading>
        <InlineForm
          buttonLabel="Play now!"
          label="Username"
          name="username"
          onChange={(e) => this._username = e.target.value}
          onClick={this.play}
        />
        <Link to="/somewhere">Take me somewhere</Link>
      </div>
    )
  }
}

const mapStateToProps = (state) => ({

})

import { actions } from '../redux/game'
const mapDispatchToProps = {
  enterGame: actions.enterGame
}

export default connect(mapStateToProps, mapDispatchToProps)(HomePage)