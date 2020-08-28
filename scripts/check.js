'use strict';

const fs = require('fs');

let buffer = fs.readFileSync(process.argv[2]);
while (buffer.length){
    const time = buffer.readInt32BE();
    const len = buffer.slice(4).readInt32BE();
    console.log(`packet at time: ${time}, length: ${len}`);
    if (buffer.length < len){
        console.log(`file truncated, ${buffer.length} < ${len}`);
    }
    buffer = buffer.slice(len + 8);
}