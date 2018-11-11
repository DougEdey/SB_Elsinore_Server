import React, {Component} from 'react'
import Col from "react-bootstrap/es/Col";
import {Card} from "@shopify/polaris";
import TempProbeModal from "./TempProbeModal";

class TempProbe extends Component {

    render() {
        return (
            <Col xs={12} md={3}>
            <Card title={this.props.probe.name} actions={[{content: "Edit", onAction: this.showEditDialog}]}>
                <Card.Section>
                    {this.props.probe.temperature} {this.props.probe.scale}
                </Card.Section>
            </Card>
                <TempProbeModal ref={(modal) => { this._modal = modal; }} probe={this.props.probe}/>
            </Col>
        )
    }

    showEditDialog = () => {
        console.log(this);
        this._modal.handleChange();
    }
}

export default TempProbe;