/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

var local;
var video;
var webRtcPeer;
var sdpOffer;
var videoStream = null;
var audioStream = null;
var iceCandidates = [];
var defaultVideoConstraints = {
	width : {
		max : 640
	},
	frameRate : {
		min : 10,
		ideal : 15,
		max : 20
	}
};
var userMediaConstraints = {
	audio : true,
	video : defaultVideoConstraints,
	fake : true
};

try {
	kurentoUtils.WebRtcPeer.prototype.server.iceServers = [];
} catch (e) {
	console.warn(e);
}

window.onload = function() {
	console = new Console("console", console);
	local = document.getElementById("local");
	video = document.getElementById("video");

	setInterval(updateCurrentTime, 100);
}

function setAudioUserMediaConstraints() {
	userMediaConstraints = {
		audio : true,
		video : false,
		fake : true
	};
}

function setVideoUserMediaConstraints() {
	userMediaConstraints = {
		audio : false,
		video : defaultVideoConstraints,
		fake : true
	};
}

function setCustomAudio(audioUrl) {
	mediaConstraints = {
		audio : false,
		video : defaultVideoConstraints,
		fake : true
	};
	getUserMedia(mediaConstraints, function(userStream) {
		videoStream = userStream;
	}, onError);

	var context = new AudioContext();
	var audioTest = document.getElementById("audioTest");
	audioTest.src = audioUrl;
	var sourceStream = context.createMediaElementSource(audioTest);
	var mixedOutput = context.createMediaStreamDestination();
	sourceStream.connect(mixedOutput);
	audioStream = mixedOutput.stream;
}

function onIceCandidate(candidate) {
	console.log('Local candidate' + JSON.stringify(candidate));
	iceCandidates.push(JSON.stringify(candidate));
}

function startSendRecv() {
	console.log("Starting WebRTC in SendRecv mode...");
	showSpinner(local, video);

    var options = {
      localVideo: local,
      remoteVideo: video,
      mediaConstraints: userMediaConstraints,
      onicecandidate : onIceCandidate
    }

	webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
		function (error) {
		  if(error) {
			  onError(error);
		  }
		  webRtcPeer.generateOffer (onOffer);
		});
}

function startSendOnly() {
	console.log("Starting WebRTC in SendOnly mode...");
	showSpinner(local);

    var options = {
      localVideo: local,
      mediaConstraints: userMediaConstraints
    }

	webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options,
		function (error) {
		  if(error) {
			  onError(error);
		  }
		  webRtcPeer.generateOffer (onOffer);
		});
}

function startRecvOnly() {
	console.log("Starting WebRTC in RecvOnly mode...");
	showSpinner(video);

    var options = {
      remoteVideo: video,
      mediaConstraints: userMediaConstraints
    }

	webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
		function (error) {
		  if(error) {
			  onError(error);
		  }
		  webRtcPeer.generateOffer (onOffer);
		});
}

function onError(error) {
	console.error(error);
}

function onOffer(error, offer) {
	console.info("SDP offer:");
	sdpOffer = offer;
}

function addIceCandidate (serverCandidate) {
	candidate = JSON.parse(serverCandidate);
	webRtcPeer.addIceCandidate(candidate, function (error) {
	   if (error) {
	     console.error("Error adding candidate: " + error);
	     return;
	   }
	});
}

function processSdpAnswer(answer) {
	var sdpAnswer = window.atob(answer);
	console.info("SDP answer:");
	console.info(sdpAnswer);

	webRtcPeer.processAnswer (sdpAnswer, function (error) {
		if (error) return console.error (error);
	});
}

function updateCurrentTime() {
	document.getElementById("currentTime").value = video.currentTime;
}

function log(text) {
	document.getElementById("status").value = text;
}

function addEventListener(type, callback) {
	video.addEventListener(type, callback, false);
}

function videoEvent(e) {
	if (!e) {
		e = window.event;
	}
	if (e.type == "playing") {
		audioTest.play();
	}
	log(e.type);
}

function addTestName(testName) {
	document.getElementById("testName").innerHTML = testName;
}

function appendStringToTitle(string) {
	document.getElementById("testTitle").innerHTML += " " + string;
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].src = '';
		arguments[i].poster = './img/webrtc.png';
		arguments[i].style.background = '';
	}
}

function stop() {
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;
	}
	hideSpinner(local, video);
	document.getElementById('status').value = '';
}
