import * as React from 'react';
import { Keys } from '../../types';
export interface Props {
    keyCode: Keys;
    handler(event: KeyboardEvent): void;
}
export default class KeypressListener extends React.Component<Props, never> {
    componentDidMount(): void;
    componentWillUnmount(): void;
    render(): null;
    private handleKeyEvent;
}
