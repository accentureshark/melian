// MongoDB initialization script for movie data
// This script sets up the initial collections and indexes for the MELIAN MCP server

print('Initializing MELIAN movies database...');

// Switch to melian_movies database
db = db.getSiblingDB('melian_movies');

// Create movies collection with sample data
db.movies.insertMany([
  {
    _id: ObjectId(),
    title: "The Matrix",
    releaseDate: "1999-03-31",
    rating: 8.7,
    genre: ["Action", "Sci-Fi"],
    description: "A computer programmer is led to fight an underground war against powerful computers who have constructed his entire reality with a system called the Matrix.",
    imdbId: "tt0133093",
    tmdbId: 603,
    director: "The Wachowskis",
    cast: ["Keanu Reeves", "Laurence Fishburne", "Carrie-Anne Moss"],
    chunks: [
      {
        text: "The Matrix follows Neo, a computer programmer who discovers reality is a simulation",
        metadata: { type: "plot", source: "tmdb" }
      },
      {
        text: "Directed by The Wachowskis, starring Keanu Reeves, Laurence Fishburne, and Carrie-Anne Moss",
        metadata: { type: "credits", source: "tmdb" }
      }
    ],
    source: "tmdb",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    _id: ObjectId(),
    title: "Inception",
    releaseDate: "2010-07-16",
    rating: 8.8,
    genre: ["Action", "Drama", "Sci-Fi"],
    description: "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.",
    imdbId: "tt1375666", 
    tmdbId: 27205,
    director: "Christopher Nolan",
    cast: ["Leonardo DiCaprio", "Marion Cotillard", "Tom Hardy"],
    chunks: [
      {
        text: "Inception explores dream-sharing technology and the concept of planting ideas in dreams",
        metadata: { type: "plot", source: "tmdb" }
      },
      {
        text: "Christopher Nolan's complex thriller about dreams within dreams, starring Leonardo DiCaprio",
        metadata: { type: "summary", source: "tmdb" }
      }
    ],
    source: "tmdb",
    createdAt: new Date(),
    updatedAt: new Date()
  }
]);

// Create indexes for efficient querying
db.movies.createIndex({ "title": "text", "description": "text" });
db.movies.createIndex({ "genre": 1 });
db.movies.createIndex({ "rating": -1 });
db.movies.createIndex({ "releaseDate": -1 });
db.movies.createIndex({ "imdbId": 1 }, { unique: true, sparse: true });
db.movies.createIndex({ "source": 1 });

// Create chunks collection for MCP resources
db.chunks.insertMany([
  {
    _id: ObjectId(),
    text: "The Matrix is a groundbreaking science fiction film that redefined the action genre with its innovative special effects and philosophical themes.",
    metadata: {
      movieTitle: "The Matrix",
      movieId: "tt0133093",
      type: "analysis",
      source: "tmdb",
      year: 1999,
      genre: ["Action", "Sci-Fi"]
    },
    embedding: null, // To be populated if vector search is implemented
    createdAt: new Date()
  },
  {
    _id: ObjectId(),
    text: "Inception features stunning visual effects and a complex narrative structure that challenges viewers to question the nature of reality and dreams.",
    metadata: {
      movieTitle: "Inception", 
      movieId: "tt1375666",
      type: "analysis",
      source: "tmdb",
      year: 2010,
      genre: ["Action", "Drama", "Sci-Fi"]
    },
    embedding: null,
    createdAt: new Date()
  }
]);

// Create indexes for chunks collection
db.chunks.createIndex({ "text": "text" });
db.chunks.createIndex({ "metadata.movieId": 1 });
db.chunks.createIndex({ "metadata.source": 1 });
db.chunks.createIndex({ "metadata.type": 1 });
db.chunks.createIndex({ "createdAt": -1 });

print('MELIAN movies database initialized successfully');
print('Collections created: movies, chunks');
print('Sample movies added: The Matrix, Inception');
print('Indexes created for efficient querying');