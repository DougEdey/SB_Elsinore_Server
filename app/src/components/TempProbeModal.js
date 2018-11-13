import React from "react";
import {Modal, FormLayout} from "@shopify/polaris";
import gql from "graphql-tag";
import { graphql, compose } from "react-apollo";
import ProbeNameField from "./formInputs/ProbeNameField";

class TempProbeModal extends React.Component {

    render() {
        const {active} = this.props;

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
        this.props.toggleEditDialog();
    };

    handleDelete = () => {
        this.props.deleteProbeMutation({variables: { id: this.props.probe.id}})
            .then(({ data }) => {
                console.log('got data', data);
            }).catch((error) => {
            console.log('there was an error sending the query', error);
        });
        this.handleChange()
    };

    handleSave = () => {
        this.handleChange()
    };

}

const DELETE_PROBE = gql`
    mutation DeleteProbe($id: Long!) {
        deleteProbe(id: $id)
    }
`;

const WithData = compose(graphql(DELETE_PROBE, {name: 'deleteProbeMutation'}))
(TempProbeModal);

export default WithData;