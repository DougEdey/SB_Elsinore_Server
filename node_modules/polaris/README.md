# Polaris

[![Build Status](https://travis-ci.org/danielgtaylor/polaris.png?branch=master)](https://travis-ci.org/danielgtaylor/polaris) [![Dependency Status](https://david-dm.org/danielgtaylor/polaris.png)](https://david-dm.org/danielgtaylor/polaris)

Lightweight backend utilities for static websites. Currently implements sending email to configured addresses from HTML forms with optional attachments. Feel free to fork and add more features.

Point your static website toward the north star.

## Installation
You can install `polaris` via NPM, the [Node.js](http://nodejs.org/) package manager.

```bash
sudo npm install -g polaris
```

## Usage
Typically you would run Polaris like any other program, with the first argument being an optional configuration file:

```bash
polaris /path/to/my/config.json
```

You can also use Polaris directly from within Node:

```javascript
var polaris = require('polaris');

polaris.config.recipients = {
  test: {
    to: ['test@gmail.com'],
    title: 'Test title',
    allowFiles: false,
    redirect: 'http://example.com/success'
  }
};

polaris.runServer();
```

It's also possible to use the Polaris request handler directly via the `http`, `connect`, or `express` modules. This means you can easily add it to an existing application as a new route:

```javascript
var express = require('express');
var polaris = require('polaris');

var app = express();

app.post('/email', polaris.handler);

app.listen(3000);
```

### Sending Emails
You can use `curl` to send a test email:

```bash
curl -X POST http://localhost:8080/ -d recipient=test -d from=test@gmail.com -d title=Testing -d message=foo
```

The same request as an HTML form would look like:

```html
<form method="post" action="http://localhost:8080/">
  <input type="hidden" name="recipient" value="test"/>
  <input type="text" name="from" placeholder="Your email"/>
  <input type="text" name="title" placeholder="Email title"/>
  <textarea name="message" placeholder="Message"></textarea>
</form>
```

### Parameters
The following parameters are available. Some are just shortcuts to adding information into the message body.

| Name      | Required | Description                                      |
| --------- | -------- | ------------------------------------------------ |
| from      | &#10003; | The sender's email address                       |
| location  |          | The sender's physical address (adds to message)  |
| message   | &#10003; | The email body                                   |
| name      |          | The sender's real name                           |
| phone     |          | The sender's phone number (adds to message)      |
| recipient | &#10003; | The recipient name from your `config.json` file. |
| title     |          | The email title / subject                        |

### Configuration
Polaris is configured via a simple JSON file. An example looks like:

```json
{
  "listen": {
    "host": "localhost",
    "port": 8080
  },
  "transport": {
    "name": "SMTP",
    "options": {
      "host": "smtp.mailgun.org",
      "secureConnection": true,
      "port": 465,
      "auth": {
        "user": "USERNAME",
        "pass": "PASSWORD"
      }
    }
  },
  "recipients": {
    "test": {
      "to": ["test@gmail.com"],
      "title": "Email subject title",
      "allowFiles": false,
      "redirect": "http://example.com/success"
    }
  }
}
```

#### Email

The `transport` options correspond to [Nodemailer](http://www.nodemailer.com/docs/usage-example) `createTransport` arguments. The configuration above is for [MailGun](https://mailgun.com/), but many possible configurations exist. For example, for [Gmail](https://www.gmail.com/):

```json
...
"transport": {
  "name": "SMTP",
  "options": {
    "service": "Gmail",
    "auth": {
      "user": "USERNAME",
      "pass": "PASSWORD"
    }
  }
},
...
```

## Deployment
Some possible deployement scenarios follow.

### Heroku
[Heroku](https://devcenter.heroku.com/articles/getting-started-with-nodejs) uses git for deployments and supports NPM dependencies. Make sure to install their tools before continuing. Then, create a new project:

```bash
mkdir server
cd server

npm init
npm install --save polaris
```

Create the following files:

##### main.js
```javascript
var polaris = require('polaris');

polaris.config.recipients = {
  test: {
    to: ['test@gmail.com'],
    title: 'Test title',
    allowFiles: false,
    redirect: 'http://example.com/success'
  }
};

polaris.runServer();
```

##### Procfile
```
web: node main.js
```

Then set up the git repo and push to deploy:

```bash
git init
git add .
git commit -m 'Initial commit'

heroku create

git push heroku master
```

### Nodejitsu
[Nodejitsu](https://www.nodejitsu.com/getting-started/) is a Node.js application hosting provider similar to Heroku. Make sure you have the `jitsu` command installed:

```bash
sudo npm install -g jitsu
```

Create the new project:

```bash
mkdir server
cd server

npm init
npm install --save polaris
```

Modify the `package.json` file to have a start script:

```json
...
"scripts": {
  "start": "node main.js"
}
...
```

Then, create the following file:

##### main.js
```javascript
var polaris = require('polaris');

polaris.config.recipients = {
  test: {
    to: ['test@gmail.com'],
    title: 'Test title',
    allowFiles: false,
    redirect: 'http://example.com/success'
  }
};

polaris.runServer();
```

Lastly, deploy using `jitsu`:

```bash
jitsu deploy
```

### Digital Ocean / AWS / Rackspace
Create a new virtual server using a recent Ubuntu image and do the following:

```bash
ssh user@yoursever

# Install dependencies
sudo apt-get install nodejs
sudo npm install -g polaris

# Create a config
sudo touch /etc/polaris.json
sudo vim /etc/polaris.json

# Setup the Upstart script
sudo cp /usr/lib/node_modules/polaris/polaris-upstart.conf /etc/init/polaris.conf

sudo touch /var/log/polaris.log
sudo chown www-data /var/log/polaris.log

# Start the service
sudo service polaris start
```

## Development
Feel free to fork and create pull requests. You should edit the `main.coffee` file since the `main.js` file is generated from it. Getting the code and building the Javascript is easy:

```bash
git clone https://github.com/path/to/your/clone

cd polaris
npm run build
```

## License
Copyright &copy; 2014 Daniel G. Taylor

http://dgt.mit-license.org/
