import {Component} from "react";
import * as React from "react";
import {Form} from "react-bootstrap";

type Props = {
    value: String;
    updateDevice: Function;
}

export class ProbeDeviceField extends Component<Props> {

    render() {
        const {updateDevice, value} = this.props;
        //const errorMessage = this.getErrorMessage(value);
        return (
            <Form.Group controlId="probe.device">
                <Form.Label>Device</Form.Label>
                <Form.Control type="text"
                    readOnly={true}
                    placeholder="Device"
                    defaultValue={value}
                    onBlur={(event: React.FocusEvent<HTMLInputElement>) => updateDevice(event.target.value)} />
            </Form.Group>
        );
    }


    getErrorMessage = (value: String) => {
        if (value == null || value.trim() === "") {
            return "Name cannot be empty";
        }
        return "";
    }
}
