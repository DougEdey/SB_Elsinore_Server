import {Component} from "react";
import * as React from "react";
import {Form} from 'react-bootstrap';
import { BigDecimal } from "../../generated/graphql"

type Props = {
    value: BigDecimal;
    scale: String;
    updateCalibration: Function;
}

export class CalibrationField extends Component<Props> {
    render() {
        const {value, scale, updateCalibration} = this.props;
        //const errorMessage = this.getErrorMessage(value);
        return (
            <Form.Group controlId="probe.device">
                <Form.Label>Calibration ({scale})</Form.Label>
                <Form.Control type="Number"
                    placeholder="Offset"
                    defaultValue={value}
                    onBlur={(event: React.FocusEvent<HTMLInputElement>) => updateCalibration(event.target.value)} />
            </Form.Group>
        );
    }
}