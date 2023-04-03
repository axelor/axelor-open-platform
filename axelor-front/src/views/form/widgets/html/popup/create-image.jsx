import { useState } from 'react';
import { filecontents, cancelEvent } from '../utils';

function ImagePopup({ commands }) {
  const [value, setValue] = useState('');

  function addImage(url) {
    commands.insertHTML(
      ' <img src="' + url + '" style="max-width:100%;max-height:20em;"> <br><br> ',
    );
    commands.closePopup();
  }

  function onImageSelect(e) {
    const files = e.target.files;
    for (let i = 0; i < files.length; ++i) {
      const file = files[i];
      filecontents(file, (type, url) => {
        if (!type.match(/^image/i)) return;
        addImage(url);
      });
    }
    cancelEvent(e);
  }

  function handleSubmit(e) {
    cancelEvent(e);
    value && addImage(!/^[a-z0-9]+:\/\//.test(value) ? `http://${value}` : value);
  }

  return (
    <div className="custom-html-editor-form" unselectable="on">
      <div className="custom-html-editor-browse">
        <span>Click or drop image</span>
        <input type="file" draggable="true" onChange={onImageSelect} />
      </div>
      <div>
        <input
          type="text"
          className="custom-html-editor-input"
          placeholder="www.example.com"
          value={value}
          onChange={e => setValue(e.target.value)}
        />
        <span onClick={handleSubmit} className="custom-html-editor-toolbar-icon">
          <i className="fa fa-check"></i>
        </span>
      </div>
    </div>
  );
}

export default ImagePopup;
