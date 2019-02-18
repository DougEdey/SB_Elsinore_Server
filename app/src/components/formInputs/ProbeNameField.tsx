import {Component} from "react";
import * as React from "react";
import {Form} from "react-bootstrap";

type Props = {
    value: string;
    updateName: Function;
}

class ProbeNameField extends Component<Props> {

    render() {
        const {updateName, value} = this.props;
        const errorMessage = this.getErrorMessage(value);
        return (
            <Form.Group controlId="probe.name">
                <Form.Label>Name</Form.Label>
                <Form.Control type="text"
                    placeholder="Probe Name"
                    defaultValue={value}
                    onBlur={(event: React.FocusEvent<HTMLInputElement>) => updateName(event.target.value)} />
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

export default ProbeNameField;