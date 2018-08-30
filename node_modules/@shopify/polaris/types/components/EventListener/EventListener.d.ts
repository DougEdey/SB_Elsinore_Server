import * as React from 'react';
export interface Props {
    event: string;
    capture?: boolean;
    passive?: boolean;
    handler(event: Event): void;
}
export default class EventListener extends React.PureComponent<Props, never> {
    componentDidMount(): void;
    componentWillUpdate(): void;
    componentDidUpdate(): void;
    componentWillUnmount(): void;
    render(): null;
    private attachListener;
    private detachListener;
}
