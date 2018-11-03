import React, {Component} from 'react'
import {MenuItem, Nav, Navbar, NavDropdown, NavItem} from "react-bootstrap";
import NavbarOffcanvas from "react-bootstrap-navbar-offcanvas";

let repo = "https://github.com/dougedey/SB_Elsinore_Server";

class Sidebar extends Component {
    render() {
        return (
            <Navbar collapseOnSelect staticTop>
                <Navbar.Header>
                    <Navbar.Brand>
                        StrangeBrew Elsinore
                    </Navbar.Brand>
                    <Navbar.Toggle/>
                </Navbar.Header>
                <NavbarOffcanvas side="left">
                    <Nav>
                        <NavItem href="https://reddit.com/r/StrangeBrew" target="_blank">Reddit</NavItem>
                        <NavDropdown title="Github" id="basic-nav-dropdown">
                            <MenuItem href={repo} target="_blank">Code</MenuItem>
                            <MenuItem href={repo + '/issues'} target="_blank">Issues</MenuItem>
                        </NavDropdown>
                    </Nav>
                </NavbarOffcanvas>
            </Navbar>
        )
    }
}

export default Sidebar