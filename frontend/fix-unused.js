const fs = require('fs');
const path = require('path');

const logPath = path.join(__dirname, 'lint-results.txt');
const log = fs.readFileSync(logPath, 'utf8');

const lines = log.split('\n');
let currentFile = '';

for (const line of lines) {
    if (line.startsWith('C:\\')) {
        currentFile = line.trim();
    } else if (line.includes('error') && line.includes('@typescript-eslint/no-unused-vars')) {
        // Extract line number, column, and variable name
        const match = line.match(/^\s*(\d+):(\d+).+'([^']+)' is defined but never used/);
        if (match && currentFile) {
            const lineNum = parseInt(match[1], 10) - 1;
            const varName = match[3];

            let content = fs.readFileSync(currentFile, 'utf8').split('\n');
            let targetLine = content[lineNum];

            // Basic regex to remove the variable, surrounding spaces, and comma
            // Works for both imports { VarName, Other } and parameter (err) => {}
            const regex = new RegExp(`\\b${varName}\\b\\s*,?|,?\\s*\\b${varName}\\b`);
            targetLine = targetLine.replace(regex, '');

            // Cleanup empty brackets for imports
            targetLine = targetLine.replace(/{\s*}/, '');
            if (targetLine.trim().startsWith('import') && targetLine.trim().endsWith(';')) {
                // if import now empty
                if (!targetLine.includes('{') || targetLine.match(/import\s+from/)) {
                    // Delete the line entirely
                    targetLine = '';
                }
            }

            content[lineNum] = targetLine;
            fs.writeFileSync(currentFile, content.join('\n'));
            console.log(`Fixed ${varName} on line ${lineNum + 1} in ${path.basename(currentFile)}`);
        }
    }
}
