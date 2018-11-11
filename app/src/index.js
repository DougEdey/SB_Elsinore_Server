import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import App from './App';
import '@shopify/polaris/styles.css';
import * as serviceWorker from './serviceWorker';
import {ApolloProvider} from 'react-apollo'
import {ApolloClient} from 'apollo-client'
import {createHttpLink} from 'apollo-link-http'
import {InMemoryCache} from 'apollo-cache-inmemory'
import {AppProvider} from "@shopify/polaris";

const httpLink = createHttpLink({
    uri: 'http://localhost:8080/graphql'
});

const client = new ApolloClient({
    link: httpLink,
    cache: new InMemoryCache()
});

ReactDOM.render(
    <AppProvider>
        <ApolloProvider client={client}>
            <App/>
        </ApolloProvider>
    </AppProvider>,
    document.getElementById('root')
);

serviceWorker.register();
