 
var sys= require('util');
var net = require('net');
var mqtt = require('mqtt');
 
// create a socket object that listens on port 5000
var io = require('socket.io').listen(5000);
 
// create an mqtt client object and connect to the mqtt broker
var client = mqtt.connect('mqtt://192.168.43.95');
 
console.log('aaaa');
io.sockets.on('connection', function (socket) {
    // socket connection indicates what mqtt topic to subscribe to in data.topic
    socket.on('subscribe', function (data) {
	console.log('bbbb');
        console.log('Subscribing to '+data.topic);
        socket.join(data.topic);
        client.subscribe(data.topic);
    });
    // when socket connection publishes a message, forward that message
    // the the mqtt broker
    socket.on('publish', function (data) {
	console.log('cccc');
        console.log('Publishing to '+data.topic);
        client.publish(data.topic,data.payload);
    });
});
client.on('connect', function() { // When connected
  // subscribe to a topic
	  client.subscribe('led', function() {
		// when a message arrives, do something with it
		client.on('message', function(topic, message, packet) {
		console.log('dddd');
		  console.log("Received '" + message + "' on '" + topic + "'");
			io.sockets.emit('mqtt',{'topic':String(topic),'payload':String(message)});		  
		console.log('eeee');
		});
	  });	
//    console.log(topic+'='+payload);

});
/*
// listen to messages coming from the mqtt broker
client.on('message', function (topic, payload, packet) {
    console.log(topic+'='+payload);
    io.sockets.emit('mqtt',{'topic':String(topic),
                            'payload':String(payload)});
});
*/
