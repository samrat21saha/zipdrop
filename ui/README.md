# ZipDrop Frontend

A Next.js application for secure file sharing with a modern, responsive UI built using Tailwind CSS.

## Features

- **Share a File Tab**: Drag-and-drop file upload with visual feedback
- **Receive a File Tab**: Enter invite codes to download files
- **Responsive Design**: Works on desktop and mobile devices
- **Modern UI**: Clean, professional interface with smooth transitions

## Tech Stack

- **Next.js 14** with App Router
- **React 18** with TypeScript
- **Tailwind CSS** for styling
- **Lucide React** for icons

## Getting Started

1. Install dependencies:
   ```bash
   npm install
   ```

2. Run the development server:
   ```bash
   npm run dev
   ```

3. Open [http://localhost:3000](http://localhost:3000) in your browser.

## Usage

### Share a File
1. Click on the "Share a File" tab
2. Either drag and drop a file onto the upload area or click to select a file
3. The selected file will be displayed with its name and size

### Receive a File
1. Click on the "Receive a File" tab
2. Enter an invite code in the input field
3. Click "Download File" to trigger the download (currently logs to console)

## Project Structure

```
├── app/
│   ├── globals.css          # Global styles and Tailwind imports
│   ├── layout.tsx           # Root layout component
│   └── page.tsx             # Main page component
├── components/
│   └── FileSharingTabs.tsx  # Main file sharing component
├── package.json             # Dependencies and scripts
├── tailwind.config.js       # Tailwind CSS configuration
├── tsconfig.json           # TypeScript configuration
└── README.md               # This file
```

## Development

- The component is fully responsive and centered on the page
- File drag-and-drop functionality includes visual feedback
- All state is managed locally using React hooks
- The UI matches modern design patterns with consistent spacing and colors
