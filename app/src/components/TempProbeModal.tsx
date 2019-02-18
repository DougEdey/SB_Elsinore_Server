import * as React from "react";
import gql from "graphql-tag";
import { graphql, compose } from "react-apollo";
import { TempRunner} from "../TemperatureProbeList";
import  ProbeNameField from "./formInputs/ProbeNameField"
import {Button, Modal, Form} from "react-bootstrap";

type Props = {
    probe: TempRunner;
    editModalActive: boolean;
    deleteProbeMutation: Function;
    toggleEditDialog: Function;
}

type State = {
    newProbe: TempRunner
}

class TempProbeModal extends React.Component<Props, State> {

    constructor(props: Props) {
        super(props);
        console.log("Active: " + props.editModalActive);
        let newProbe: TempRunner = Object.assign({}, props.probe);
        this.setState({newProbe: newProbe});
    }

    updateName = (newName: string) => {
        console.log("Updated name: "  + newName);
        this.setState(({newProbe}) => ({newProbe: {...newProbe, name: newName}}));
    }

    render() {
        const {editModalActive, probe} = this.props;
        return (
            <Modal show={editModalActive}>
                <Modal.Header>
                    <Modal.Title>Edit {probe.name}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
            
                <Form>
                    <ProbeNameField value={this.props.probe.name} updateName={this.updateName} />
                </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.handleSave}>Save</Button>
                    <Button onClick={this.handleDelete}>Delete</Button>
                </Modal.Footer>
            </Modal>
        );
    }


    private handleSave = () => {
        console.log("Updated: " +  this.state.newProbe);
        this.props.toggleEditDialog();
    };

    private handleDelete = () => {
        this.props.deleteProbeMutation({variables: { id: this.props.probe.id}})
            .then(({ data }) => {
                console.log('got data', data);
            }).catch((error) => {
            console.log('there was an error sending the query', error);
        });
        this.props.toggleEditDialog();
    };
}

const DELETE_PROBE = gql`
    mutation DeleteProbe($id: Long!) {
        deleteProbe(id: $id)
    }
`;

const WithData = compose(graphql(DELETE_PROBE, {name: 'deleteProbeMutation'})) (TempProbeModal);

export default WithData as TempProbeModal;