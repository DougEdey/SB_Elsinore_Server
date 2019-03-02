import * as React from "react";
import gql from "graphql-tag";
import { graphql, compose } from "react-apollo";
import { ProbeNameField, ProbeDeviceField, ScaleField, CalibrationField } from './formInputs';
import {Button, Modal, Form} from "react-bootstrap";
import { TemperatureModelInput, BigDecimal } from "../generated/graphql";

type ModalProps = {
    probe: TemperatureModelInput;
    editModalActive: boolean;
    deleteProbeMutation: Function;
    toggleEditDialog: Function;
    refetch: Function;
}

type State = {
    newProbe: TemperatureModelInput
}

class TempProbeModal extends React.Component<ModalProps, State> {

    constructor(props: ModalProps) {
        super(props);
        let newProbe: TemperatureModelInput = Object.assign({}, props.probe);
        delete newProbe["__typename"]
        this.state = { newProbe: newProbe };
    }

    updateName = (newName: string) => {
        console.log("Updated name: "  + newName);
        this.setState(({newProbe}) => ({newProbe: {...newProbe, name: newName}}));
    }

    updateDevice = (newDevice: string) => {
        console.log("Updated device: "  + newDevice);
        this.setState(({newProbe}) => ({newProbe: {...newProbe, device: newDevice}}));
    }

    updateScale = (newScale: string) => {
        console.log("Updated scale: "  + newScale);
        this.setState(({newProbe}) => ({newProbe: {...newProbe, scale: newScale}}));
    }

    updateCalibration = (newCalibration: BigDecimal) => {
        if (newCalibration == "") {
            newCalibration = null;
        }
        console.log("Updated calibration: "  + newCalibration);
        this.setState(({newProbe}) => ({newProbe: {...newProbe, calibration: newCalibration}}));
    }


    render() {
        const {editModalActive, probe} = this.props;

        return (
            <Modal show={editModalActive} onHide={this.props.toggleEditDialog} >
                <Modal.Header>
                    <Modal.Title>Edit {probe.name}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
            
                <Form>
                    <ProbeNameField value={probe.name} updateName={this.updateName} />
                    <Form.Row>
                        <CalibrationField value={probe.calibration} updateCalibration={this.updateCalibration} />
                        <ScaleField value={probe.scale} updateScale={this.updateScale} />
                    </Form.Row>
                    <Form.Group>
                        <ProbeDeviceField value={probe.device} updateDevice={this.updateDevice} />
                    </Form.Group>
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
        console.log("Updated: " +  JSON.stringify(this.state.newProbe));
        this.props.updateProbeMutation({variables: { probe: this.state.newProbe}})
            .then(({ data }) => {
                console.log('got data', data);
            }).catch((error) => {
            console.log('there was an error sending the query', error);
        });
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

const UPDATE_PROBE = gql`
    mutation UpdateProbe($probe: TemperatureModelInput!) {
        updateProbe(probe: $probe) {
            name
        }
    }
`;
const WithData = compose(
    graphql(DELETE_PROBE, {name: 'deleteProbeMutation'}), 
    graphql(UPDATE_PROBE, {name: 'updateProbeMutation'})
) (TempProbeModal);

export default WithData as TempProbeModal;