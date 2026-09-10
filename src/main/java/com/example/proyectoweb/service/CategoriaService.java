@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository repository;

    public List<Categoria> listar(){
        return repository.findAll();
    }

    public Categoria buscarPorId(Long id){

        return repository.findById(id).orElse(null);
    }

    public Categoria guardar(Categoria categoria){

        return repository.save(categoria);
    }

    public Categoria actualizar(Categoria categoria){

        return repository.save(categoria);
    }

    public void eliminar(Long id){

        if(repository.existsById(id)){
            repository.deleteById(id);
        }
    }

}