import * as React from 'react';
import { GetTemperatureProbeList as QUERY } from './queries/temperatureProbes';
import { Query } from 'react-apollo';
import TempProbe from "./components/TempProbe";

type Response = {
    tempRunners : TempRunner[];
};

type TempRunner = {
    id: number,
    name: string,
    tempF: number,
    temperature: number
    scale: string,
    started: boolean
};

export const TemperatureProbeList: React.FunctionComponent<Response> = (props: Response) => {
    return (
        <Query query={QUERY} >
            {({ loading, data, error }) => {
                if (loading) return <div>Loading</div>;
                if (error) return <h1>ERROR: {error.message}</h1>;
                if (!data) return <div>no data</div>;

                const { tempRunners } = data;
                return (tempRunners.map((probe: TempRunner) => <TempProbe key={probe.id.toString()} probe={probe}/>));
            }}
        </Query>
    );
};

export default TemperatureProbeList;
export {TempRunner};
