package br.feevale.fevasDB;

public class ClasseDeNegocioImplementation implements ClasseDeNegocio {

    @Override
    public void beforeInsert(Conexao cnx, Object registro) throws FevasDBException {}

    @Override
    public void beforeUpdate(Conexao cnx, Object registro) throws FevasDBException {}

    @Override
    public void beforeDelete(Conexao cnx, Object registro) throws FevasDBException {}

    @Override
    public void afterInsert(Conexao cnx, Object registro) throws FevasDBException {}

    @Override
    public void afterUpdate(Conexao cnx, Object registro) throws FevasDBException {}

    @Override
    public void afterDelete(Conexao cnx, Object registro) throws FevasDBException {}
    
}
