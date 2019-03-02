import {Component} from "react";
import * as React from "react";
import {Form} from 'react-bootstrap';

type Props = {
    value: String;
    updateCalibration: Function;
}

export class CalibrationField extends Component<Props> {
    render() {
        const {value, updateCalibration} = this.props;
        //const errorMessage = this.getErrorMessage(value);
        return (
            <Form.Group controlId="probe.device">
                <Form.Label>Calibration</Form.Label>
                <Form.Control type="Number"
                    placeholder="Offset"
                    defaultValue={value}
                    onBlur={(event: React.FocusEvent<HTMLInputElement>) => updateCalibration(event.target.value)} />
            </Form.Group>
        );
    }
}