package Pachube;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PachubeFactory {

	/**
	 * Creates a feed from a String
	 * @param p Pachube object which is the gateway to the pachube service
	 * @param s String which represents  a feed object, this String should be well formed eeml.
	 * @return Feed manufactured from the String
	 */
	public static Feed toFeed(Pachube p, String s) {
		Feed f = new Feed(p);

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new ByteArrayInputStream(s
					.getBytes("UTF-8")));
			doc.getDocumentElement().normalize();
			getMisc(f, doc);
			getData(f, doc);
			getlocation(f, doc);

		} catch (Exception e) {
			e.printStackTrace();
		}
		f.setCreated(true);
		return f;
	}

	/**
	 * Method to retrieve data from eeml
	 * @param f
	 * @param doc
	 * @throws DOMException
	 * @throws PachubeException
	 */
	private static void getMisc(Feed f, Document doc) throws DOMException,
			PachubeException {
		NodeList nodeLst = doc.getElementsByTagName("environment");
		Node c = nodeLst.item(0);

		NamedNodeMap n = c.getAttributes();
		Node h = n.getNamedItem("updated");
		if (h != null) {
			f.setUpdated(h.getNodeValue());
		}
		f.setId((n.getNamedItem("id").getNodeValue()));
		if (c.getNodeType() == Node.ELEMENT_NODE) {

			Element e = (Element) c;

			NodeList fstNmElmntLst = e.getElementsByTagName("title");
			Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
			NodeList fstNm = fstNmElmnt.getChildNodes();
			f.setTitle(((Node) fstNm.item(0)).getNodeValue());

			fstNmElmntLst = e.getElementsByTagName("status");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			fstNm = fstNmElmnt.getChildNodes();
			f.setStatus(Status.valueOf(((Node) fstNm.item(0)).getNodeValue()));

			fstNmElmntLst = e.getElementsByTagName("description");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);

			if (fstNmElmnt != null) {
				fstNm = fstNmElmnt.getChildNodes();
				h = ((Node) fstNm.item(0));
				f.setDescription(h.getNodeValue());
			}

			fstNmElmntLst = e.getElementsByTagName("website");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);

			if (fstNmElmnt != null) {
				fstNm = fstNmElmnt.getChildNodes();
				try {
					f
							.setWebsite(new URL(((Node) fstNm.item(0))
									.getNodeValue()));
				} catch (Exception e3) {

				}
			}

			fstNmElmntLst = e.getElementsByTagName("feed");
			fstNmElmnt = (Element) fstNmElmntLst.item(0);
			fstNm = fstNmElmnt.getChildNodes();
			try {
				f.setFeed(new URL(((Node) fstNm.item(0)).getNodeValue()));
			} catch (Exception e3) {

			}

		}

	}

	/**
	 * Method to retrieve data from eeml
	 * @param f
	 * @param doc
	 * @throws PachubeException
	 */
	private static void getlocation(Feed f, Document doc)
			throws PachubeException {
		NodeList nodeLst = doc.getElementsByTagName("location");
		Node c = nodeLst.item(0);
		Location l = new Location();
		NamedNodeMap n;
		if (c != null) {
			n = c.getAttributes();
			l
					.setDomain(Domain.valueOf(n.getNamedItem("domain")
							.getNodeValue()));

			Node h = n.getNamedItem("exposure");
			
			if (h != null) {
				l.setExposure(Exposure.valueOf(h.getNodeValue()));
			}

			h = n.getNamedItem("disposition");
			if (h != null) {
				l.setExposure(Exposure.valueOf(h.getNodeValue()));
			}

			if (c.getNodeType() == Node.ELEMENT_NODE) {

				Element e = (Element) c;

				NodeList fstNmElmntLst = e.getElementsByTagName("name");
				Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
				NodeList fstNm;
				if (fstNmElmnt != null) {
					fstNm = fstNmElmnt.getChildNodes();
					l.setName(((Node) fstNm.item(0)).getNodeValue());
				}

				fstNmElmntLst = e.getElementsByTagName("lat");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);
				fstNm = fstNmElmnt.getChildNodes();
				h = ((Node) fstNm.item(0));
				if (h != null) {
					l.setLat(h.getNodeValue());
				}

				fstNmElmntLst = e.getElementsByTagName("lon");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);
				fstNm = fstNmElmnt.getChildNodes();

				h = ((Node) fstNm.item(0));
				if (h != null) {
					l.setLon(h.getNodeValue());
				}

				fstNmElmntLst = e.getElementsByTagName("ele");
				fstNmElmnt = (Element) fstNmElmntLst.item(0);

				if (fstNmElmnt != null) {
					fstNm = fstNmElmnt.getChildNodes();
					h = ((Node) fstNm.item(0));
					if (h != null) {
						l.setElevation(h.getNodeValue());
					}
				}
			}

		}
		// System.out.println(l.toString());
		f.setLocation(l);

	}

	/**
	 * Method to retrieve data from eeml
	 * @param f
	 * @param doc
	 */
	private static void getData(Feed f, Document doc) {
		NodeList nodeLst = doc.getElementsByTagName("data");
		Data d;
		for (int i = 0; i < nodeLst.getLength(); i++) {
			d = new Data();
			Node fstNode = nodeLst.item(i);

			if (fstNode.getNodeType() == Node.ELEMENT_NODE) {

				Element fstElmnt = (Element) fstNode;

				NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("tag");
				Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
				if (fstNmElmnt != null) {
					NodeList fstNm = fstNmElmnt.getChildNodes();
					d.setTag(((Node) fstNm.item(0)).getNodeValue());
				}
				d.setId(fstNode.getAttributes().getNamedItem("id")
						.getNodeValue());
				NodeList lstNmElmntLst = fstElmnt.getElementsByTagName("value");
				Element lstNmElmnt = (Element) lstNmElmntLst.item(0);
				NodeList lstNm = lstNmElmnt.getChildNodes();
				Node h = ((Node) lstNm.item(0));
				if (h != null) {
					d.setValue(h.getNodeValue());
				}

				h = lstNmElmnt.getAttributes().getNamedItem("minValue");
				if (h != null) {
					d.setMinValue(h.getNodeValue());
				}

				h = lstNmElmnt.getAttributes().getNamedItem("maxValue");
				if (h != null) {
					d.setMaxValue(h.getNodeValue());
				}

				f.addData(d);
			}

		}
	}

	/**
	 * Creates a Trigger from a String
	 * @param s String which represents  a trigger object, this String should be well formed eeml.
	 * @return Trigger array manufactured from the String
	 */
	public static Trigger[] toTrigger(String s) {
		ArrayList<Trigger> tL = new ArrayList<Trigger>();

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new ByteArrayInputStream(s
					.getBytes("UTF-8")));
			doc.getDocumentElement().normalize();

			/*****************************************************
			 * 
			 */

			NodeList nodeLst = doc.getElementsByTagName("datastream-trigger");

			for (int i = 0; i < nodeLst.getLength(); i++) {
				Trigger t = new Trigger();
				Node c = nodeLst.item(i);
				NamedNodeMap n;
				if (c != null) {

					if (c.getNodeType() == Node.ELEMENT_NODE) {

						Element e = (Element) c;

						NodeList fstNmElmntLst = e.getElementsByTagName("id");
						Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
						NodeList fstNm;
						if (fstNmElmnt != null) {
							fstNm = fstNmElmnt.getChildNodes();
							t.setID(Integer.parseInt((((Node) fstNm.item(0))
									.getNodeValue())));
						}

						fstNmElmntLst = e.getElementsByTagName("url");
						fstNmElmnt = (Element) fstNmElmntLst.item(0);
						fstNm = fstNmElmnt.getChildNodes();
						Node h = ((Node) fstNm.item(0));
						if (h != null) {
							try {
								t.setUrl(new URL(h.getNodeValue()));
							} catch (MalformedURLException err) {
								t.setUrl(null);
							}
						}

						fstNmElmntLst = e.getElementsByTagName("trigger-type");
						fstNmElmnt = (Element) fstNmElmntLst.item(0);
						fstNm = fstNmElmnt.getChildNodes();

						h = ((Node) fstNm.item(0));
						if (h != null) {
							t.setType(TriggerType.valueOf(h.getNodeValue()));
						}

						fstNmElmntLst = e
								.getElementsByTagName("threshold-value");
						fstNmElmnt = (Element) fstNmElmntLst.item(0);
						fstNm = fstNmElmnt.getChildNodes();

						h = ((Node) fstNm.item(0));
						if (h != null) {
							t
									.setThreshold(Double.parseDouble(h
											.getNodeValue()));
						}

						fstNmElmntLst = e
								.getElementsByTagName("environment-id");
						fstNmElmnt = (Element) fstNmElmntLst.item(0);
						fstNm = fstNmElmnt.getChildNodes();

						h = ((Node) fstNm.item(0));
						if (h != null) {
							t.setEnv_id(Integer.parseInt(h.getNodeValue()));
						}

						fstNmElmntLst = e.getElementsByTagName("stream-id");
						fstNmElmnt = (Element) fstNmElmntLst.item(0);
						fstNm = fstNmElmnt.getChildNodes();

						h = ((Node) fstNm.item(0));
						if (h != null) {
							t.setStream_id(Integer.parseInt(h.getNodeValue()));
						}
					}

				}

				tL.add(t);
			}

			/******************************************************************
			 * 
			 */

		} catch (Exception e) {
			e.printStackTrace();
		}

		return tL.toArray(new Trigger[0]);
	}

}
