package davi.nunes.ribeiro.galeria;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AlertDialogLayout;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    List<String> photos = new ArrayList<>();
    MainAdapter mainAdapter;
    static int RESULT_TAKE_PICTURE = 1;
    static int RESULT_REQUEST_PERMISSION = 2;
    String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.tbMain);
        setSupportActionBar(toolbar); // coloca a toolbar como a actionbar padrao da tela

        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); // acessa o diretorio de fotos
        File[] files = dir.listFiles(); // lista de fotos salvas

        for (int i = 0; i < files.length; i++){
            photos.add(files[i].getAbsolutePath()); // adiciona as fotos salvas na lista de fotos da main activity
        }

        mainAdapter = new MainAdapter(MainActivity.this, photos);

        RecyclerView rvGallery = findViewById(R.id.rvGallery);
        rvGallery.setAdapter(mainAdapter);

        float w = getResources().getDimension(R.dimen.itemWidth); // pega a largura do item
        int numberOfColumns = Utils.calculateNoOfColumns(MainActivity.this, w); // calcula quantas colunas a activity vai ter baseado na largura do item
        GridLayoutManager gridLayoutManager = new GridLayoutManager(MainActivity.this, numberOfColumns);
        rvGallery.setLayoutManager(gridLayoutManager); // configura o recycler view como grid

        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA); // adiciona a permissao de usar a camera

        checkForPermissions(permissions); // verifica se a permissao da camera foi concedida
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_tb, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){ // aciona sempre qe um item da toolbar é acionado
        switch (item.getItemId()){
            case R.id.opCamera: // caso o item seja o item da camera
                dispatchTakePictureIntent(); // dispara a camera do celular
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void startPhotoActivity(String photoPath){
        Intent i = new Intent(MainActivity.this, PhotoActivity.class);
        i.putExtra("photo_path", photoPath); // coloca o caminho da foto que deverá ser aberta na photoactivity
        startActivity(i);
    }

    private void dispatchTakePictureIntent(){
        File f = null; // cria um arquivo vazio
        try{
            f = createImageFile(); // cria uma imagem
        } catch (IOException e){
            Toast.makeText(MainActivity.this, "Não foi possível criar o arquivo", Toast.LENGTH_LONG).show();
            return;
        }

        currentPhotoPath = f.getAbsolutePath(); // salva o caminho da imagem no currentPhotoPath
        if(f != null){
            Uri fUri = FileProvider.getUriForFile(MainActivity.this, "davi.nunes.ribeiro.galeria.fileprovider", f); // gera um URI para a imagem
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); // cria um intent para abrir a camera
            i.putExtra(MediaStore.EXTRA_OUTPUT, fUri);
            startActivityForResult(i, RESULT_TAKE_PICTURE); // abre a camera e espera a foto como resultado
        }
    }

    private File createImageFile() throws IOException{
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); // pega a hora que a imagem foi tirada
        String imageFileName = "JPEG_" + timeStamp; // junta a hora com o prefixo JPEG_ para o nome do arquivo
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File f = File.createTempFile(imageFileName, ".jpg", storageDir);
        return f;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_TAKE_PICTURE){
            if (resultCode == Activity.RESULT_OK){ // caso a foto tenha sido tirada
                photos.add(currentPhotoPath); // adiciona o caminho da foto na lista de fotos
                mainAdapter.notifyItemInserted(photos.size()-1); // avisa o main adapter de que uma foto foi inserida para atualizar o recycler view
            }
            else { // caso a foto nao tenha sido tirada
                File f = new File(currentPhotoPath);
                f.delete(); // deleta o arquivo criado para conter a foto
            }
        }
    }

    private void checkForPermissions(List<String> permissions){
        List<String> permissionsNotGranted = new ArrayList<>();

        for (String permission : permissions){
            if (!hasPermission(permission)){ // verifica se cada permissão está garantida
                permissionsNotGranted.add(permission); // adiciona cada permissão não garantida a uma lista
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (permissionsNotGranted.size() > 0){
                requestPermissions(permissionsNotGranted.toArray(new String[permissionsNotGranted.size()]), RESULT_REQUEST_PERMISSION); // solicita as permissões ao usuário
            }
        }
    }

    private boolean hasPermission(String permission){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            return ActivityCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_GRANTED; // retorna se uma permissão ja foi garantida ou não
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        final List<String> permissionsRejected = new ArrayList<>();
        if (requestCode == RESULT_REQUEST_PERMISSION){
            for (String permission : permissions){
                if(!hasPermission(permission)){ // verifica se cada permissão foi concedida
                    permissionsRejected.add(permission);
                }
            }
        }
        if (permissionsRejected.size() > 0){ // se ainda houver alguma permissão não garantida
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(shouldShowRequestPermissionRationale(permissionsRejected.get(0))){ // exibe uma mensagem e pede novamente para conceder as permissões necessárias
                    new AlertDialog.Builder(MainActivity.this).setMessage("Para usar esse app é necessário conceder essas permissões:").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]), RESULT_REQUEST_PERMISSION);
                        }
                    }).create().show();

                }
            }
        }
    }

}