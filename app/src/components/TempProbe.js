import React, {Component} from 'react'
import Col from "react-bootstrap/es/Col";
import {Card} from "@shopify/polaris";
import TempProbeModal from "./TempProbeModal";

class TempProbe extends Component {

    state = { active: false};

    render() {
        return (
            <Col xs={12} md={3}>
            <Card title={this.props.probe.name} actions={[{content: "Edit", onAction: this.toggleEditDialog}]}>
                <Card.Section>
                    {this.props.probe.temperature} {this.props.probe.scale}
                </Card.Section>
            </Card>
                <TempProbeModal probe={this.props.probe} active={this.state.active}
                    toggleEditDialog={this.toggleEditDialog}/>
            </Col>
        )
    }

    toggleEditDialog = () => {
        console.log(this);
        this.setState({active: !this.state.active});
    }
}

export default TempProbe;