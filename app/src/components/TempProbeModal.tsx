import * as React from "react";
import gql from "graphql-tag";

import { graphql, compose } from "react-apollo";
import { ProbeNameField, ProbeDeviceField, ScaleField, CalibrationField, CutoffField } from './formInputs';
import { Row, Col, Button, Modal, Form } from "react-bootstrap";
import { TemperatureModelInput, BigDecimal } from "../generated/graphql";

type ModalProps = {
    probe: TemperatureModelInput;
    editModalActive: boolean;
    deleteProbeMutation: Function;
    updateProbeMutation: Function;
    toggleEditDialog: () => void;
    refetch: Function;
}

type State = {
    newProbe: TemperatureModelInput;
    refreshComponents: boolean;
}

class TempProbeModal extends React.Component<ModalProps, State> {

    constructor(props: ModalProps) {
        super(props);
        let newProbe: TemperatureModelInput = Object.assign({}, props.probe);
        delete newProbe["__typename"]
        this.state = { newProbe: newProbe, refreshComponents: false };
    }

    updateName = (newName: string) => {
        console.log("Updated name: "  + newName);
        this.setState(({newProbe}) => ({newProbe: {...newProbe, name: newName}}));
    }

    updateScale = (newScale: string) => {
        console.log("Updated scale: "  + newScale);
        this.setState(({newProbe}) => ({newProbe: {...newProbe, scale: newScale}}));
        this.forceUpdate();
    }

    updateCalibration = (newCalibration: BigDecimal) => {
        if (newCalibration == "") {
            newCalibration = null;
        }
        console.log("Updated calibration: "  + newCalibration);
        this.setState(({newProbe}) => ({newProbe: {...newProbe, calibration: newCalibration}, refreshComponents: !this.state.refreshComponents}));
    }

    updateCutoff = (newCutoff: BigDecimal) => {
        if (newCutoff == "") {
            newCutoff = null;
        }
        console.log("Updated cutoff: "  + newCutoff);
        this.setState(({newProbe}) => ({newProbe: {...newProbe, cutoffTemp: newCutoff}}));
    }


    render() {
        const {editModalActive} = this.props;
        let {newProbe} = this.state;

        return (
            <Modal show={editModalActive} onHide={this.props.toggleEditDialog} >
                <Modal.Header>
                    <Modal.Title>Edit {newProbe.name}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
            
                <Form>
                    <ProbeNameField value={newProbe.name} updateName={this.updateName} />
                    
                    <Row>
                        <Col>
                            <ProbeDeviceField value={newProbe.device} />
                        </Col>
                        <Col>
                            <ScaleField 
                                value={newProbe.scale}
                                updateScale={this.updateScale} />
                        </Col>
                    </Row>
                    <CalibrationField 
                        value={newProbe.calibration}
                        scale={newProbe.scale}
                        updateCalibration={this.updateCalibration} />
                    
                    <CutoffField 
                        value={newProbe.cutoffTemp}
                        scale={newProbe.scale}
                        updateCutoff={this.updateCutoff} />
                        
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
    graphql(UPDATE_PROBE, {name: 'updateProbeMutation'}),
) (TempProbeModal);

export default WithData as TempProbeModal;