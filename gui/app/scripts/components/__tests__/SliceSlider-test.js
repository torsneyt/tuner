/** @jsx React.DOM */

jest.dontMock('../SliceSlider.jsx')
    .dontMock('jquery')
    .dontMock('jquery-ui');

var Cortex, React, SliceSlider, TestUtils;

describe('SliceSlider', function() {
  beforeEach(function() {
    React = require('react/addons');
    SliceSlider = require('../SliceSlider.jsx');
    TestUtils = React.addons.TestUtils;
  });

  it('renders the correct parameter name', function() {

    var slice = {
      name: 'x1',
      value: 0.5
    };
    var zoom = {
      lowValue: 0.0,
      highValue: 1.0
    };
    var slider = TestUtils.renderIntoDocument(
      <SliceSlider slice={slice} zoom={zoom} />
    );

    // Make sure the label has the correct value
    var label = TestUtils.findRenderedDOMComponentWithTag(slider, 'label');
    expect(label.getDOMNode().textContent).toEqual('x1');
  });
  
});