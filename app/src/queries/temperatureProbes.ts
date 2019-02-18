import gql from "graphql-tag";

export const GetTemperatureProbeList = gql`
  query TemperatureProbeList {
      tempRunners {
          id
          name
          tempF
          temperature
          scale
          started
      }
  }
`;
