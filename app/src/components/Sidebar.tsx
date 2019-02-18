import React, {Component} from 'react'
import {Nav, Navbar} from "react-bootstrap";

let repo = "https://github.com/dougedey/SB_Elsinore_Server";
let reddit = "https://reddit.com/r/strangebrew";

class Sidebar extends Component {

    render() {
        return (
            <Navbar bg="light" expand="lg">
                <Navbar.Brand href="#home">StrangeBrew Elsinore</Navbar.Brand>
                <Navbar.Toggle aria-controls="basic-navbar-nav" />
                <Navbar.Collapse>
                    <Nav className="mr-auto">
                        <Nav.Link href={repo} target="_blank">GitHub</Nav.Link>
                        <Nav.Link href={reddit} target="_blank">Reddit</Nav.Link>
                    </Nav>
                </Navbar.Collapse>
            </Navbar>
        );
    }
}

export default Sidebar