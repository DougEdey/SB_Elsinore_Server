import {Component} from "react";
import * as React from "react";
import {Form} from 'react-bootstrap';
import { Maybe, BigDecimal } from "../../generated/graphql"

type Props = {
    value: BigDecimal;
    scale: Maybe<String>;
    updateCutoff: Function;
}

export class CutoffField extends Component<Props> {
    render() {
        const {value, scale, updateCutoff} = this.props;
        //const errorMessage = this.getErrorMessage(value);
        return (
            <Form.Group controlId="probe.cutoff">
                <Form.Label>Cutoff Temperature ({scale})</Form.Label>
                <Form.Control type="Number"
                    placeholder="Cutoff"
                    defaultValue={value}
                    onBlur={(event: React.FocusEvent<HTMLInputElement>) => updateCutoff(event.target.value)} />
            </Form.Group>
        );
    }
}