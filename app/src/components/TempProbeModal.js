import React from "react";
import {Modal, FormLayout, TextField} from "@shopify/polaris";
import ProbeNameField from "./formInputs/ProbeNameField";


class TempProbeModal extends React.Component {
    state = {
        active: false,
    };

    render() {
        const {active} = this.state;

        return (
                <Modal
                    open={active}
                    onClose={this.handleChange}
                    title={`Edit ${this.props.probe.name}`}
                    primaryAction={{
                        content: 'Save',
                        onAction: this.handleSave,
                    }}
                    secondaryActions={[
                        {
                            content: 'Delete',
                            onAction: this.handleDelete,
                        },
                    ]}
                >
                    <Modal.Section>
                        <FormLayout>
                            <ProbeNameField probe={this.props.probe} />
                        </FormLayout>
                    </Modal.Section>
                </Modal>
        );
    }

    handleChange = () => {
        this.setState(({active}) => ({active: !active}));
    };

    handleDelete = () => {
        const DELETE_PROBE = JSON.stringify({query: `
        mutation {
            deleteProbe(id: ${this.props.probe.id})
        }
    `});

        console.log(DELETE_PROBE);
        fetch('http://localhost:8080/graphql', {
            method: 'post',
            headers: {'Content-Type': 'application/json'},
            body: DELETE_PROBE,
        })
            .then((response) => response.json())
            .then((json) => console.log(JSON.stringify(json, null, 2)));
        this.handleChange()
    };

    handleSave = () => {
        this.handleChange()
    };

}
export default TempProbeModal;