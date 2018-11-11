import React, {Component} from "react";
import TempProbe from "./TempProbe";
import {Query} from "react-apollo";
import gql from "graphql-tag";

const FEED_QUERY = gql`

{
    tempRunners
    {
        id
        name
        temperature
        scale
    }
}
`;

class TempProbeList extends Component {

    render() {


        return (
            <Query query={FEED_QUERY} variables={"foo"}>
                {({ loading, error, data }) => {
                    if (loading) return <p>Loading...</p>;
                    if (error) return <p>Error :(</p>;

                    return data.tempRunners.map(probe => <TempProbe key={probe.id} probe={probe}/>)}
                }
            </Query>
        )
    }
}

export default TempProbeList