package br.feevale.fevasDB;

public class FevasDBException extends Exception {

	private static final long serialVersionUID = 8136419859511335445L;

	public FevasDBException( String msg ) {
		super( msg );
	}
	
	public FevasDBException( Exception e ) {
		super( e );
	}
}