import { configure } from 'react-apollo-form';
import * as index from '../../ApolloClient'

const jsonSchema = require('../../../mutations/apollo-form-json-schema.json');

export const ApplicationForm = configure<ApolloFormMutationNames>({
    // tslint:disable-next-line:no-any
    client: index.client as any,
    jsonSchema
});

