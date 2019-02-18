import React, {Component} from 'react'
import Col from 'react-bootstrap/Col';
import Card from 'react-bootstrap/Card'

import {TempRunner} from "../TemperatureProbeList";
import ErrorBoundary from "./ErrorBoundary";
import TempProbeModal from "./TempProbeModal";


const initialState = { editModalActive: false};

type State = Readonly<typeof initialState>;
type Props = {
    probe: TempRunner;
}

class TempProbe extends Component<Props, State> {

    readonly state: State = initialState;

    private toggleEditDialog = (prevState: State) => {
        console.log(this);
        console.log("Prev State: " + prevState.editModalActive);
        this.setState({editModalActive: !prevState.editModalActive});
    };

    private renderTemperature = (temperature: number, scale: string) =>{
        if (temperature == null) {
            return "N/A";
        }
        return temperature.toString() + scale;
    };


    render() {
        const {probe} = this.props;
        return (
            <ErrorBoundary>
                <Col xs={12} md={3}>
                    <Card>
                        <Card.Header onDoubleClick={() => this.toggleEditDialog(this.state)}>{probe.name}</Card.Header>
                        <Card.Body>
                            <Card.Text>
                                {this.renderTemperature(probe.temperature, probe.scale)}
                            </Card.Text>
                        </Card.Body>

                    </Card>
                </Col>
                <TempProbeModal probe={probe} state={this.state} editModalActive={this.state.editModalActive} toggleEditDialog={() => this.toggleEditDialog(this.state)}/>
            </ErrorBoundary>
        );
    };



}

export default TempProbe ;
export { State };