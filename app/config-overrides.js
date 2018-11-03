module.exports = config => {
    require('react-app-rewire-postcss')(config, {
        plugins: loader => [
            require('postcss-flexbugs-fixes'),
            require('postcss-preset-env')({
                autoprefixer: {
                    flexbox: 'no-2009',
                },
                stage: 3,
                // This is the one line that is different from the built-in config
                features: {'custom-properties': false}
            }),
        ]
    });

    return config;
};