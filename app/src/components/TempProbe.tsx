import * as React from 'react'
import Col from 'react-bootstrap/Col';
import Card from 'react-bootstrap/Card'

import {TempRunner} from "../TemperatureProbeList";
import ErrorBoundary from "./ErrorBoundary";
import TempProbeModal  from "./TempProbeModal";
import { Query } from 'react-apollo';
import gql from "graphql-tag";
import { TemperatureModelInput } from "../generated/graphql";
import { client } from '../ApolloClient';


const initialState = { editModalActive: false};

type State = Readonly<typeof initialState>;
type Props = {
    probe: TempRunner;
}

const GET_PROBE = gql`
    query GetProbe($probe_id: Long!) {
        temperatureModel(id: $probe_id) {
            id
            name
            device
            hidden
            cutoffTemp
            scale
            calibration
        }
    }
`;

class TempProbe extends React.Component<Props, State> {

    readonly state: State = initialState;

    private toggleEditDialog = (prevState: State) => {
        console.log(this);
        console.log("Prev State: " + prevState.editModalActive);
        this.setState({editModalActive: !prevState.editModalActive});
        client.reFetchObservableQueries();
    };

    private renderTemperature = (temperature: number, scale: string) =>{
        if (temperature == null) {
            return "N/A";
        }
        return temperature.toString() + scale;
    };

    render() {
        const {probe} = this.props;
        console.log(GET_PROBE)
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
                <Query query={GET_PROBE} variables={{ probe_id: probe.id }} fetchPolicy={'network-only'} >
                    {({ loading, data, error }) => {
                        if (loading) return <div>Loading</div>;
                        if (error) return <h1>ERROR: {error.message}</h1>;
                        if (!data) return <div>no data</div>;
                        let temperatureModel: TemperatureModelInput = data.temperatureModel

                        return <TempProbeModal probe={temperatureModel} state={this.state} 
                            editModalActive={this.state.editModalActive} 
                            toggleEditDialog={() => this.toggleEditDialog(this.state)}
                            />
                    }}
                </Query>
            </ErrorBoundary>
        );
    };
}

export default TempProbe;
export { State };