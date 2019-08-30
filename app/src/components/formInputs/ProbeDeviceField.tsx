import {Component} from "react";
import * as React from "react";
import {Form} from "react-bootstrap";
import { Maybe } from '../../generated/graphql';

type Props = {
    value: Maybe<String>;
}

export class ProbeDeviceField extends Component<Props> {

    render() {
        let {value} = this.props;
        if (!(value instanceof String)) {
            value = "";
        }

        //const errorMessage = this.getErrorMessage(value);
        return (
            <Form.Control type="text"
                readOnly={true}
                placeholder="Device"
                defaultValue={value.toString()} />
        );
    }


    getErrorMessage = (value: String) => {
        if (value == null || value.trim() === "") {
            return "Name cannot be empty";
        }
        return "";
    }
}
