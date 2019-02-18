import * as React from 'react';
import { render } from 'react-dom';
import { ApolloProvider } from 'react-apollo';
import { App } from './App';
import { client } from './ApolloClient'



const WrappedApp = (
    <ApolloProvider client={client}>
        <App />
    </ApolloProvider>
);

render(WrappedApp, document.getElementById('app'));

