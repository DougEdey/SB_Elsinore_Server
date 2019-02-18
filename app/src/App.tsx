import * as React from 'react';
import TemperatureProbeList from './TemperatureProbeList';
import Sidebar from "./components/Sidebar";

export const App = () => [
    <Sidebar />,
    <TemperatureProbeList />
];