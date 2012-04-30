package de.fu_berlin.inf.dpp.whiteboard.gef.model;

import org.apache.batik.util.SVGConstants;

import de.fu_berlin.inf.dpp.whiteboard.sxe.records.DocumentRecord;

/*
 * Note: As I take SVG Tiny as a reference, svg:svg elements cannot be nested
 */
public class SVGRootRecord extends SVGRectRecord {

	public SVGRootRecord(DocumentRecord documentRecord) {
		super(documentRecord);
		setName(SVGConstants.SVG_SVG_TAG);
		setNs(SVGConstants.SVG_NAMESPACE_URI);
	}

	public boolean isComposite() {
		return true;
	}

}
